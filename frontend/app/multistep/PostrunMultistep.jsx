import Multistep from 'react-multistep';
import React from 'react';
import axios from 'axios';
import PropTypes from 'prop-types';
import MetadataComponent from './postrun/MetadataComponent.jsx';
import SourceComponent from './postrun/SourceComponent.jsx';
import DependencyComponent from './postrun/DependencyComponent.jsx';
import CompletionComponent from './postrun/CompletionComponent.jsx';

class PostrunMultistep extends React.Component {
    static propTypes = {
        match: PropTypes.object.isRequired
    };

    constructor(props){
        super(props);
        this.state = {
            postrunMetadata: {},
            postrunSource: "",
            originalDependencies: [],
            updatedDependencies: [],
            postrunList: [],
            currentEntry: null,
            loading: false,
            loadingError: null
        };

        this.metaValueWasSet = this.metaValueWasSet.bind(this);
        this.sourceValueWasSet = this.sourceValueWasSet.bind(this);
        this.dependencyValueWasSet = this.dependencyValueWasSet.bind(this);
    }

    //given a list of full dependency record, just return a list of dependsOn
    getDependsId(recordList){
        return recordList ? recordList.map(entry=>entry.dependsOn) : [];
    }

    componentWillMount(){
        if(this.props.match && this.props.match.params && this.props.match.params.itemid && this.props.match.params.itemid!=="new"){
            this.setState({currentEntry: this.props.match.params.itemid, loading: true}, ()=>{
                const promiseList = [
                    axios.get("/api/postrun/" + this.state.currentEntry),
                    axios.get("/api/postrun/" + this.state.currentEntry + "/source"),
                    axios.get("/api/postrun/" + this.state.currentEntry + "/depends"),
                    axios.get("/api/postrun")
                ];

                Promise.all(promiseList)
                    .then(results=>{
                        console.log("got results");
                        this.setState({
                            postrunMetadata: results[0].data.result,
                            postrunSource: results[1] ? results[1].data : "",
                            originalDependencies: results[2] ? Object.assign([],this.getDependsId(results[2].data.result)) : [],
                            updatedDependencies: results[2] ? this.getDependsId(results[2].data.result) : [],
                            postrunList: results[3] ? results[3].data.result : [],
                            loading: false
                        }, ()=>console.log("done"))
                    .catch(error=>{
                        console.error(error);
                        this.setState({loading: false, loadingError: error})
                    });
                })
            })
        }
    }

    metaValueWasSet(newvalue){
        this.setState({postrunMetadata: Object.assign(this.state.postrunMetadata, newvalue)});
    }

    sourceValueWasSet(newvalue){
        this.setState({postrunSource: newvalue});
    }

    dependencyValueWasSet(newvalue){
        this.setState({updatedDependencies: newvalue});
    }

    render(){
        const steps = [
            {
                name: "Review source code",
                component: <SourceComponent sourceCode={this.state.postrunSource} valueWasSet={this.sourceValueWasSet}/>
            },
            {
                name: "Postrun Metadata",
                component: <MetadataComponent title={this.state.postrunMetadata.title}
                                              description={this.state.postrunMetadata.description}
                                              runnable={this.state.postrunMetadata.runnable}
                                              version={this.state.postrunMetadata.version}
                                              valueWasSet={this.metaValueWasSet}/>
            },
            {
                name: "Dependencies",
                component: <DependencyComponent actionsList={this.state.postrunList}
                                                selectedDependencies={this.state.updatedDependencies}
                                                valueWasSet={this.dependencyValueWasSet}
                                                currentEntry={this.state.currentEntry}
                />
            },
            {
                name: "Summary",
                component: <CompletionComponent postrunMetadata={this.state.postrunMetadata}
                                                postrunSource={this.state.postrunSource}
                                                currentEntry={this.state.currentEntry}
                                                actionList={this.state.postrunList}
                                                selectedDependencies={this.state.updatedDependencies}
                                                originalDependencies={this.state.originalDependencies}
                />
            }
        ];
        return <Multistep steps={steps} showNavigation={true}/>
    }
}

export default PostrunMultistep;