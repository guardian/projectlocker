import Multistep from 'react-multistep';
import React from 'react';
import axios from 'axios';
import PropTypes from 'prop-types';
import MetadataComponent from './postrun/MetadataComponent.jsx';
import SourceComponent from './postrun/SourceComponent.jsx';
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
            currentEntry: null,
            loading: false,
            loadingError: null
        };

        this.metaValueWasSet = this.metaValueWasSet.bind(this);
        this.sourceValueWasSet = this.sourceValueWasSet.bind(this);
    }

    componentWillMount(){
        if(this.props.match && this.props.match.params && this.props.match.params.itemid && this.props.match.params.itemid!=="new"){
            this.setState({currentEntry: this.props.match.params.itemid, loading: true}, ()=>{
                const promiseList = [
                    axios.get("/api/postrun/" + this.state.currentEntry),
                    axios.get("/api/postrun/" + this.state.currentEntry + "/source")
                ];

                Promise.all(promiseList)
                    .then(results=>{
                        console.log("got results");
                        this.setState({
                            postrunMetadata: results[0].data.result,
                            postrunSource: results[1] ? results[1].data : "",
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
        console.log("metadataValueWasSet");
        this.setState({postrunMetadata: Object.assign(this.state.postrunMetadata, newvalue)});
    }

    sourceValueWasSet(newvalue){
        console.log("sourceValueWasSet");
        this.setState({postrunSource: newvalue});
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
                name: "Summary",
                component: <CompletionComponent postrunMetadata={this.state.postrunMetadata}
                                                postrunSource={this.state.postrunSource}
                                                currentEntry={this.state.currentEntry}
                />
            }
        ];
        return <Multistep steps={steps} showNavigation={true}/>
    }
}

export default PostrunMultistep;