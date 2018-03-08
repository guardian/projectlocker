import Multistep from 'react-multistep';
import React from 'react';
import axios from 'axios';
import PropTypes from 'prop-types';

import ProjectTypeComponent from './projecttype/ProjectTypeComponent.jsx';
import ProjectTypeCompletionComponent from './projecttype/CompletionComponent.jsx';
import PostrunActionComponent from './projecttype/PostrunActionComponent.jsx';

class ProjectTypeMultistep extends React.Component {
    static propTypes = {
        match: PropTypes.object.required
    };

    constructor(props){
        super(props);

        this.state = {
            projectType: null,
            currentEntry: null,
            postrunList: [],
            loading: false,
            loadingError: null,
            selectedPostruns: []
        }
    }

    componentWillMount(){
        if(this.props.match && this.props.match.params && this.props.match.params.itemid && this.props.match.params.itemid!=="new"){
            this.setState({currentEntry: this.props.match.params.itemid}, ()=>this.loadPostrunActions())
        } else {
            this.loadPostrunActions();
        }
    }

    loadPostrunActions(){
        //load in the list of postrun actions from the server
        this.setState({loading: true},()=> {
            let promiseList = [axios.get("/api/postrun")];
            if(this.state.currentEntry) promiseList.push(axios.get("/api/projecttype/" + this.state.currentEntry + "/postrun"));

            Promise.all(promiseList)
                .then(responseList=>{
                    this.setState({
                        postrunList: responseList[0].data.result,
                        selectedPostruns: responseList[1] ? responseList[1].data.result : [],
                        loading: false
                    })
                })
                .catch(error=>this.setState({loading:false, loadingError: error}));
        });
    }

    render(){
        const steps = [
            {
                name: 'Project type',
                component: <ProjectTypeComponent
                    currentEntry={this.state.currentEntry}
                    valueWasSet={(newtype)=>{this.setState({projectType: newtype})}}
                />
            },
            {
                name: 'Postrun Actions',
                component: <PostrunActionComponent actionsList={this.state.postrunList}
                                                   valueWasSet={value=>{
                                                       console.log("postrun valueWasSet: ", value);
                                                       this.setState({selectedPostruns: value})
                                                   }}
                                                   selectedEntries={this.state.selectedPostruns}/>
            },
            {
                name: 'Confirm',
                component: <ProjectTypeCompletionComponent projectType={this.state.projectType}
                                                           currentEntry={this.state.currentEntry}
                                                           postrunActions={this.state.postrunList}
                                                           selectedPostruns={this.state.selectedPostruns}
                />
            }
        ];
        return(<Multistep showNavigation={true} steps={steps}/>);
    }
}

export default ProjectTypeMultistep;
