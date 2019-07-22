import React from 'react';
import axios from 'axios';
import SortableTable from "react-sortable-table";
import GeneralListComponent from "./GeneralListComponent.jsx";
import ProjectTypeView from "./EntryViews/ProjectTypeView.jsx";
import WorkingGroupEntryView from "./EntryViews/WorkingGroupEntryView.jsx";
import CommissionEntryView from "./EntryViews/CommissionEntryView.jsx";
import ProjectEntryFilterComponent from "./filter/ProjectEntryFilterComponent.jsx";
import {Link} from "react-router-dom";
import ErrorViewComponent from "./common/ErrorViewComponent.jsx";

class ProjectValidationView extends React.Component {
    constructor(props){
        super(props);

        this.state = {
            loading: false,
            totalProjectCount: 0,
            problemProjects: [],
            lastError: null,
            hasRun: false
        };

        this.columns = [
            {
                header: "Id",
                key: "id",
                defaultSorting: "desc",
                dataProps: { className: 'align-right'},
                headerProps: { className: 'dashboardheader'}
            },
            GeneralListComponent.standardColumn("Title","title"),
            {
                header: "Pluto project",
                key: "vidispineId",
                render: this.getPlutoLink,
                headerProps: { className: 'dashboardheader'}
            },
            {
                header: "Project type",
                key: "projectTypeId",
                render: (typeId)=><ProjectTypeView entryId={typeId}/>,
                headerProps: {className: 'dashboardheader'}
            },
            GeneralListComponent.dateTimeColumn("Created", "created"),
            GeneralListComponent.standardColumn("Owner","user"),
            {
                header: "Working group",
                key: "workingGroupId",
                render: typeId=><WorkingGroupEntryView entryId={typeId}/>,
                headerProps: {className: 'dashboardheader'}
            },
            {
                header: "Commission",
                key: "commissionId",
                render: typeId=><CommissionEntryView entryId={typeId}/>,
                headerProps: {className: 'dashboardheader'}
            },
            this.actionIcons(),
            {
                header: "",
                key: "id",
                headerProps: {className: 'dashboardheader'},
                render: projid=><a target="_blank" href={"pluto:openproject:" + projid}>Open project</a>
            }
        ];

        this.style = {
            backgroundColor: '#eee',
            border: '1px solid black',
            borderCollapse: 'collapse'
        };

        this.iconStyle = {
            color: '#aaa',
            paddingLeft: '5px',
            paddingRight: '5px'
        };

        this.runValidation = this.runValidation.bind(this);
    }

    breakdownPathComponents() {
        return this.props.location.pathname.split('/')
    }

    /* this method supplies the edit and delete icons. Can't be static as <Link> relies on the object context to access
 * history. */
    actionIcons() {
        const componentName = this.breakdownPathComponents()[1];
        return {
            header: "",
            key: "id",
            render: (id) => <span className="icons" style={{display: this.state.isAdmin ? "inherit" : "none"}}>
                    <Link to={"/" + componentName + "/" + id}><img className="smallicon" src="/assets/images/edit.png"/></Link>
                    <Link to={"/" + componentName + "/" + id + "/delete"}><img className="smallicon" src="/assets/images/delete.png"/></Link>
            </span>
        }
    }

    getFilterComponent(){
        return <p/>
    }

    runValidation(){
        this.setState({loading: true}, ()=>axios.post("/api/project/validate").then(result=>{
            this.setState({loading: false, hasRun: true, totalProjectCount: result.data.totalProjectsCount, problemProjects:result.data.failedProjectsList})
        }).catch(err=>{
            console.error(err);
            this.setState({loading: false, hasRun: true, lastError:err})
        }));
    }

    itemLimitWarning(){
        if(this.state.maximumItemsLoaded)
            return <p className="warning-text"><i className="fa-info fa" style={{marginRight: "0.5em", color: "orange"}}/>Maximum of {GeneralListComponent.ITEM_LIMIT} items have been loaded. Use filters to narrow this down.</p>
        else
            return <p style={{margin: 0}}/>
    }

    showRunning(){
        if(this.state.loading) {
            return <p className="warning-text">
                <img src="/assets/images/uploading.svg" className="smallicon" style={{display: this.state.loading ? "inline" : "none", verticalAlign: "middle", marginRight: "1em"}}/>Searching
                for unlinked projects...</p>
        } else {
            return <p/>
        }
    }

    showResult(){
        if(!this.state.loading && !this.state.hasRun){
            return <p style={{marginLeft: "1em"}}>
                <i className="fa fa-3x fa-info-circle validation-status-text" style={{ color: "blue", verticalAlign: "middle"}}/>
                This function checks that all of the projects in the database exist at their correct filesystem location.<br/>
                Click "Run Validation" to perform the scan.
            </p>
        }
        if(!this.state.loading && this.state.hasRun){
            if(this.state.totalProjectCount===0){
                return <p><i className="fa fa-3x fa-exclamation-triangle validation-status-text" style={{color:"orange", verticalAlign: "middle"}}/>Hmmm, there were no projects found to scan.</p>
            }
            if(this.state.problemProjects.length===0){
                return <p><i className="fa fa-3x fa-smile-o validation-status-text" style={{color: "#f9e100", verticalAlign: "middle"}}/>Hooray, no unlinked projects found! {this.state.totalProjectCount} projects checked successfully</p>
            } else {
                return <p><i className="fa fa-3x fa-exclamation-triangle validation-status-text" style={{color:"orange", verticalAlign: "middle"}}/>{this.state.problemProjects.length} projects were not found on their correct storage locations.<br/>Affected projects are shown in the table below.</p>
            }
        }
    }

    showError(){
        if(!this.state.loading && this.state.lastError){
            return <ErrorViewComponent error={this.state.lastError}/>
        }
    }
    render(){
        return <div>
            <span className="list-title"><h2 className="list-title">Validate Projects</h2></span>
            {this.getFilterComponent()}
            {this.itemLimitWarning()}
            {this.showRunning()}
            {this.showError()}

            <span className="banner-control">
                <button id="newElementButton" onClick={this.runValidation}>Run validation</button>
            </span>

            {this.showResult()}

            <SortableTable
                data={ this.state.problemProjects}
                columns={this.columns}
                style={this.state.problemProjects.length>0 ? this.style : {"display": "none"}}
                iconStyle={this.iconStyle}
                tableProps={ {className: "dashboardpanel"} }
            />

            </div>
    }
}

export default ProjectValidationView;