import React from 'react';
import axios from 'axios';
import GeneralDeleteComponent from './GeneralDeleteComponent.jsx';
import SummaryComponent from '../multistep/postrun/SummaryComponent.jsx';

class PostrunDeleteComponent extends GeneralDeleteComponent {
    constructor(props){
        super(props);
        this.itemClass = "Postrun action";
        this.endpoint = "/api/postrun";
    }

    componentWillMount(){
        super.componentWillMount();

        this.setState({actionsList: [], dependentActions: []}, ()=>{
            Promise.all([axios.get("/api/postrun"), axios.get("/api/postrun/" + this.props.entryId + "/depends")]).then(responses=>{
                this.setState({
                    actionsList: responses[0].data.result,
                    dependentActions: responses[1].data.result
                })
            })
        })

    }
    getSummary(){
        return <SummaryComponent
            title={this.state.selectedItem.title}
            description={this.state.selectedItem.description}
            actionList={this.state.actionsList}
            selectedActions={this.state.dependentActions}
        />;
    }

}

export default PostrunDeleteComponent;