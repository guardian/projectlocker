import Multistep from 'react-multistep';
import React from 'react';
import axios from 'axios';
import PlutoLinkageComponent from "./commissioncreate/PlutoLinkageComponent.jsx";
import CommissionTitleComponent from "./commissioncreate/CommissionTitleComponent.jsx";
import CommissionCompletionComponent from "./commissioncreate/CommissionCompletionComponent.jsx";

class CommissionCreateMultistep extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            wgList: [],
            selectedWorkingGroup: null,
            title: "",
        }
    }

    componentDidMount() {
        Promise.all([
            axios.get("/api/pluto/workinggroup")
        ]).then(responses=>{
            const firstWorkingGroup = responses[0].data.result.length ? responses[0].data.result[0].id : null;
            this.setState({
                wgList: responses[0].data.result, selectedWorkingGroup: firstWorkingGroup
            });
        }).catch(error=>{
            console.error(error);
            this.setState({lastError: error});
        });
    }

    getWarnings(){
        let list=[];
        if(this.props.selectedWorkingGroupId===null || this.props.selectedWorkingGroupId===0)
            list.push("You need to select a working group");
        if(this.props.title===null || this.props.title==="")
            list.push("You need to choose a title");
        return list;
    }

    render() {
        const steps = [
            {
                name: "Working Group",
                component: <PlutoLinkageComponent valueWasSet={(updatedContent)=>this.setState({selectedWorkingGroup: updatedContent.workingGroupRef})}
                                                  workingGroupList={this.state.wgList}
                                                  currentWorkingGroup={this.state.selectedWorkingGroup }
                />
            },
            {
                name: "Title",
                component: <CommissionTitleComponent selectionUpdated={(newValue)=>this.setState({title: newValue})} projectName={this.state.title}/>
            },
            {
                name: "Summary",
                component: <CommissionCompletionComponent title={this.state.title} wgList={this.state.wgList} workingGroupId={this.state.selectedWorkingGroup}/>
            }
        ];

        return <Multistep showNavigation={true} steps={steps}/>;
    }
}

export default CommissionCreateMultistep;
